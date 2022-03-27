package checkers.dto.request;


import checkers.model.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStatusRequest {
    private Status status;
}
