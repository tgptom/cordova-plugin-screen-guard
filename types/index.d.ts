export interface ScreenGuardError {
    code: 'INVALID_ARGUMENT' | 'NOT_MAIN_ACTIVITY' | 'NO_ACTIVITY' | 'CLEAR_UNSUPPORTED' | 'SET_FAILED' | 'UNKNOWN';
    message: string;
}

export interface ScreenGuardEvent extends Event {
    /** iOS only. */
    reason: 'resignActive' | 'screenshot' | 'becomeActive';
}

export interface ScreenGuardPlugin {
    setSecure(enabled: boolean, success?: () => void, error?: (err: ScreenGuardError) => void): void;
}

declare global {
    interface Window {
        ScreenGuard: ScreenGuardPlugin;
    }
    const ScreenGuard: ScreenGuardPlugin;
    interface DocumentEventMap {
        screenguard: ScreenGuardEvent;
    }
}
