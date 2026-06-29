package uiys.common.data;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public final class ValidateResult {

	private String filed;
	private Object val;
	private String msg;

	public ValidateResult(String filed, Object val, String msg) {
		this.filed = filed;
		this.val = val;
		this.msg = msg;
	}

}


