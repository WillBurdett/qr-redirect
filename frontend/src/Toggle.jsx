import './css/Toggle.css';

export default function Toggle({ checked, onChange, disabled = false, label }) {
    return (
        <button
            type="button"
            role="switch"
            aria-checked={checked}
            aria-label={label}
            disabled={disabled}
            className={`toggle ${checked ? 'toggle--on' : 'toggle--off'}`}
            onClick={() => onChange(!checked)}
        >
            <span className="toggle__knob" />
        </button>
    );
}