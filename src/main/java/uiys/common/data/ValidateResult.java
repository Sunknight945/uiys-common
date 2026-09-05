package uiys.common.data;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public final class ValidateResult {

	private String field;
	private Object val;
	private String msg;

	public ValidateResult(String field, Object val, String msg) {
		this.field = field;
		this.val = val;
		this.msg = msg;
	}

}


