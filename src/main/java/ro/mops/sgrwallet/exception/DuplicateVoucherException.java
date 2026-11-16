package ro.mops.sgrwallet.exception;

public class DuplicateVoucherException extends RuntimeException {
    public DuplicateVoucherException(String message) {
        super(message);
    }
}
