package uiys.common.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Setter
@Getter
public class ValidationException extends RuntimeException {

	private List<ValidateResult> result;

	public ValidationException(List<ValidateResult> result) {
		this.result = result;
	}


}

