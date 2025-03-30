package kr.hhplus.ecommerce.common.exception;

import kr.hhplus.ecommerce.common.support.Status;

public class DomainException extends ApplicationException {
    public DomainException(Status status) {
        super(status);
    }

    public DomainException(Status status, String message) {
        super(status, message);
    }

    public DomainException(Status status, Throwable cause) {
        super(status, cause);
    }

    public DomainException(Status status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
