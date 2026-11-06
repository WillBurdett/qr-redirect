import { useState } from 'react';

const PLAN_TYPES = ["PILOT", "STARTER", "BRANCH", "AGENCY", "CUSTOM"];

export default function AddCompanyForm({ onSubmit }) {
    const [name, setName] = useState('');
    const [planType, setPlanType] = useState('');

    async function handleSubmit(e) {
        e.preventDefault();
        if (await onSubmit(name, planType)) {
            setName('');
            setPlanType('');
        }
    }

    return (
        <section className="card">
            <h2 className="text-base font-semibold">Add company</h2>
            <form onSubmit={handleSubmit} className="mt-4 flex gap-2">
                <input className="input min-w-0 flex-1" value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="New company name" required maxLength={128} />
                <select className="input min-w-0 flex-1" value={planType}
                    onChange={(e) => setPlanType(e.target.value)} required>
                    <option value="" disabled>Plan Type</option>
                    {PLAN_TYPES.map((type) => (
                        <option key={type} value={type}>{type}</option>
                    ))}
                </select>
                <button className="btn btn-primary">Add</button>
            </form>
        </section>
    );
}