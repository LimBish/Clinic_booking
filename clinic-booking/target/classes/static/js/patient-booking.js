        const { useState, useEffect, useCallback } = React;

        let doctors = [];
        try {
            doctors = JSON.parse(DOCTORS_DATA || '[]') || [];
        } catch (e) {
            console.error('Invalid DOCTORS_DATA JSON:', e);
            doctors = [];
        }

        // Day-of-week index: 0=Sun,1=Mon,...6=Sat → doctor working days are like "MONDAY"
        const DOW_MAP = { SUNDAY:0, MONDAY:1, TUESDAY:2, WEDNESDAY:3, THURSDAY:4, FRIDAY:5, SATURDAY:6 };

        function formatTime12Hour(timeStr) {
                    if (!timeStr) return '';
                    const [hRaw, mRaw] = String(timeStr).split(':');
                    const h = Number(hRaw);
                    const m = Number(mRaw ?? 0);
                    if (Number.isNaN(h) || Number.isNaN(m)) return timeStr;
                    const ampm = h >= 12 ? 'PM' : 'AM';
                    const hour12 = h % 12 || 12;
                    return `${hour12}:${String(m).padStart(2, '0')} ${ampm}`;
                }

        function DoctorCard({ doc, selected, onSelect }) {
            return (
                <div
                    className={`p-3 border-bottom ${selected
                        ? 'bg-primary bg-opacity-10 border-start border-4 border-primary'
                        : 'border-start border-4 border-white'}`}
                    style={{ cursor: 'pointer' }}
                    onClick={() => onSelect(doc)}
                >
                    <div className="d-flex align-items-center gap-3">
                        <div
                            className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
                            style={{ width: 46, height: 46, fontSize: 18 }}
                        >
                            {doc.user?.fullName?.[0] ?? 'D'}
                        </div>

                        <div className="flex-grow-1 min-width-0">
                            <div className="fw-semibold">Dr. {doc.user?.fullName}</div>
                            <div className="small text-muted">{doc.specialization}</div>

                            {doc.consultationFee > 0 && (
                                <div className="small text-success fw-semibold">
                                    Rs {doc.consultationFee} / visit
                                </div>
                            )}

                            {doc.workingDays?.length > 0 && (
                                <div className="mt-1 d-flex flex-wrap gap-1">
                                    {['MON','TUE','WED','THU','FRI','SAT','SUN'].map(d => {
                                        const full = {
                                            MON:'MONDAY',TUE:'TUESDAY',WED:'WEDNESDAY',
                                            THU:'THURSDAY',FRI:'FRIDAY',SAT:'SATURDAY',SUN:'SUNDAY'
                                        }[d];
                                        const active = doc.workingDays.includes(full);
                                        return (
                                            <span
                                                key={d}
                                                className={`badge ${active ? 'bg-primary' : 'bg-light text-muted'}`}
                                                style={{ fontSize: '.65rem' }}
                                            >
                                                {d}
                                            </span>
                                        );
                                    })}
                                </div>
                            )}
                        </div>

                        {selected && <i className="bi bi-check-circle-fill text-primary flex-shrink-0"></i>}
                    </div>
                </div>
            );
        }

        function SlotPicker({ doctorId, workingDays, onDateChange, onSlotSelect, selectedSlot, selectedDate }) {
            const [leaveDates, setLeaveDates] = useState([]);
            const [slots, setSlots] = useState([]);
            const [loading, setLoading] = useState(false);
            const [noSchedule, setNoSchedule] = useState(false);

            const workingDowSet = new Set((workingDays || []).map(d => DOW_MAP[d]));
            const today = new Date().toISOString().split('T')[0];

            // Fetch leave dates whenever doctor changes
            useEffect(() => {
                if (!doctorId) return;
                fetch(`${AVAIL_URL}?doctorId=${doctorId}`)
                    .then(r => r.json())
                    .then(data => setLeaveDates(data.leaveDates || []))
                    .catch(() => setLeaveDates([]));
            }, [doctorId]);

            const fetchSlots = useCallback(async (date) => {
                setLoading(true);
                setNoSchedule(false);
                setSlots([]);
                onSlotSelect('', date);
                try {
                    const res = await fetch(`${SLOTS_URL}?doctorId=${doctorId}&date=${date}`);
                    const data = await res.json();
                    if (!Array.isArray(data) || data.length === 0) setNoSchedule(true);
                    setSlots(Array.isArray(data) ? data : []);
                } catch {
                    setNoSchedule(true);
                }
                setLoading(false);
            }, [doctorId, onSlotSelect]);

            const handleDateChange = (e) => {
                const date = e.target.value;
                onDateChange(date);
                if (date) fetchSlots(date);
            };

            const isDateBlocked = (dateStr) => {
                if (!dateStr) return false;
                const d = new Date(dateStr + 'T00:00:00');
                const dow = d.getDay();
                if (workingDowSet.size > 0 && !workingDowSet.has(dow)) return true;
                if (leaveDates.includes(dateStr)) return true;
                return false;
            };

            const dateWarning = selectedDate && isDateBlocked(selectedDate)
                ? leaveDates.includes(selectedDate)
                    ? 'Doctor is on leave on this date.'
                    : 'Doctor does not work on this day.'
                : null;

            return (
                <div>
                    <div className="mb-3">
                        <label className="form-label fw-semibold">Select Date</label>

                        {workingDays?.length > 0 && (
                            <div className="mb-2 d-flex flex-wrap gap-1">
                                <span className="small text-muted me-1">Works on:</span>
                                {workingDays.map(d => (
                                    <span
                                        key={d}
                                        className="badge bg-success bg-opacity-10 text-success"
                                        style={{ fontSize: '.7rem' }}
                                    >
                                        {d.charAt(0) + d.slice(1).toLowerCase()}
                                    </span>
                                ))}
                            </div>
                        )}

                        <input
                            type="date"
                            className={`form-control ${dateWarning ? 'is-invalid' : ''}`}
                            min={today}
                            value={selectedDate}
                            onChange={handleDateChange}
                        />

                        {dateWarning && <div className="invalid-feedback d-block">{dateWarning}</div>}

                        {leaveDates.length > 0 && (
                            <div className="form-text">
                                <i className="bi bi-info-circle me-1"></i>
                                Doctor is on leave: {leaveDates.slice(0,3).join(', ')}
                                {leaveDates.length > 3 ? ` +${leaveDates.length-3} more` : ''}
                            </div>
                        )}
                    </div>

                    {loading && (
                        <div className="text-center py-3 text-muted">
                            <div className="spinner-border spinner-border-sm me-2"></div>Loading slots...
                        </div>
                    )}

                    {!loading && selectedDate && !dateWarning && noSchedule && (
                        <div className="alert alert-warning py-2 d-flex gap-2 align-items-center">
                            <i className="bi bi-calendar-x"></i>
                            <span>No available slots on this date. Try a different day.</span>
                        </div>
                    )}

                    {!loading && slots.length > 0 && !dateWarning && (
                        <div>
                            <label className="form-label fw-semibold">
                                Available Time Slots
                                <span className="badge bg-primary ms-2">{slots.length} open</span>
                            </label>
                            <div className="d-flex flex-wrap gap-2">
                                {slots.map(slot => (
                                    <button
                                        type="button"
                                        key={formatTime12Hour(slot)}
                                        className={`btn btn-sm ${selectedSlot === slot ? 'btn-primary' : 'btn-outline-primary'}`}
                                        onClick={() => onSlotSelect(slot, selectedDate)}
                                    >
                                        {formatTime12Hour(slot)}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            );
        }

        function BookingApp() {
            const [selectedDoctor, setSelectedDoctor] = useState(null);
            const [selectedDate, setSelectedDate] = useState('');
            const [selectedSlot, setSelectedSlot] = useState('');
            const [reason, setReason] = useState('');
            const [submitting, setSubmitting] = useState(false);

            const handleDoctorSelect = (doc) => {
                setSelectedDoctor(doc);
                setSelectedDate('');
                setSelectedSlot('');
            };

            const handleSubmit = (e) => {
                e.preventDefault();
                if (!selectedDoctor || !selectedSlot || !selectedDate) {
                    alert('Please select a doctor, date, and time slot.');
                    return;
                }
                setSubmitting(true);

                const form = document.createElement('form');
                form.method = 'POST';
                form.action = BOOK_URL;

                const fields = {
                    doctorId: selectedDoctor.id,
                    appointmentDate: selectedDate,
                    appointmentTime: selectedSlot,
                    reason
                };

                Object.entries(fields).forEach(([k, v]) => {
                    const inp = document.createElement('input');
                    inp.type = 'hidden';
                    inp.name = k;
                    inp.value = v ?? '';
                    form.appendChild(inp);
                });

                document.body.appendChild(form);
                form.submit();
            };

            return (
                <div className="row g-4">
                    {/* Doctor list */}
                    <div className="col-lg-5">
                        <div className="card border-0 shadow-sm">
                            <div className="card-header bg-white border-0 py-3 d-flex justify-content-between align-items-center">
                                <h5 className="mb-0 fw-semibold">Select Doctor</h5>
                                <span className="badge bg-light text-dark">{doctors.length} available</span>
                            </div>

                            <div className="card-body p-0" style={{ maxHeight: 520, overflowY: 'auto' }}>
                                {doctors.length === 0 ? (
                                    <div className="text-center py-5 text-muted">
                                        <i className="bi bi-person-x" style={{ fontSize: '3rem' }}></i>
                                        <p className="mt-2">No doctors available</p>
                                    </div>
                                ) : (
                                    doctors.map(doc => (
                                        <DoctorCard
                                            key={doc.id}
                                            doc={doc}
                                            selected={selectedDoctor?.id === doc.id}
                                            onSelect={handleDoctorSelect}
                                        />
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Booking panel */}
                    <div className="col-lg-7">
                        <div className="card border-0 shadow-sm">
                            <div className="card-header bg-white border-0 py-3">
                                {selectedDoctor ? (
                                    <div>
                                        <h5 className="fw-semibold mb-0">
                                            Book with Dr. {selectedDoctor.user?.fullName}
                                        </h5>
                                        {selectedDoctor.bio && (
                                            <small className="text-muted">{selectedDoctor.bio}</small>
                                        )}
                                    </div>
                                ) : (
                                    <h5 className="fw-semibold mb-0 text-muted">← Select a doctor to continue</h5>
                                )}
                            </div>

                            <div className="card-body">
                                {!selectedDoctor ? (
                                    <div className="text-center py-5 text-muted">
                                        <i className="bi bi-arrow-left-circle" style={{ fontSize: '3rem' }}></i>
                                        <p className="mt-3">Choose a doctor from the list to see their available slots</p>
                                    </div>
                                ) : (
                                    <form onSubmit={handleSubmit}>
                                        <SlotPicker
                                            doctorId={selectedDoctor.id}
                                            workingDays={selectedDoctor.workingDays}
                                            selectedDate={selectedDate}
                                            selectedSlot={selectedSlot}
                                            onDateChange={setSelectedDate}
                                            onSlotSelect={(slot, date) => {
                                                setSelectedSlot(slot);
                                                setSelectedDate(date);
                                            }}
                                        />

                                        <div className="mb-3 mt-3">
                                            <label className="form-label fw-semibold">Reason for Visit</label>
                                            <textarea
                                                className="form-control"
                                                rows="3"
                                                value={reason}
                                                onChange={e => setReason(e.target.value)}
                                                placeholder="Briefly describe your symptoms or reason for the visit..."
                                            />
                                        </div>

                                        {selectedSlot && (
                                            <div className="alert alert-success py-2 d-flex align-items-center gap-2 mb-3">
                                                <i className="bi bi-calendar-check-fill"></i>
                                                <span>
                                                    <strong>{selectedDate}</strong> at <strong>{formatTime12Hour(selectedSlot)}</strong>
                                                    {selectedDoctor.consultationFee > 0 && (
                                                        <span className="ms-2 text-muted">
                                                            · Rs {selectedDoctor.consultationFee} fee
                                                        </span>
                                                    )}
                                                </span>
                                            </div>
                                        )}

                                        <button
                                            type="submit"
                                            className="btn btn-primary w-100 fw-semibold"
                                            disabled={!selectedSlot || submitting}
                                        >
                                            {submitting ? (
                                                <>
                                                    <span className="spinner-border spinner-border-sm me-2"></span>
                                                    Booking...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-calendar-plus me-2"></i>
                                                    Confirm Appointment
                                                </>
                                            )}
                                        </button>
                                    </form>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            );
        }

        ReactDOM.createRoot(document.getElementById('booking-app')).render(<BookingApp />);