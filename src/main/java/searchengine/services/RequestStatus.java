package searchengine.services;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class RequestStatus {
    @Getter
    private static HttpStatus status;

    public static void setStatus(int statusCode) {
        switch (statusCode) {
            case 400 -> RequestStatus.status = HttpStatus.BAD_REQUEST;
            case 401 -> RequestStatus.status = HttpStatus.UNAUTHORIZED;
            case 403 -> RequestStatus.status = HttpStatus.FORBIDDEN;
            case 404 -> RequestStatus.status = HttpStatus.NOT_FOUND;
            case 405 -> RequestStatus.status = HttpStatus.METHOD_NOT_ALLOWED;
            case 500 -> RequestStatus.status = HttpStatus.INTERNAL_SERVER_ERROR;
            default -> RequestStatus.status = HttpStatus.OK;
        }
    }
}
