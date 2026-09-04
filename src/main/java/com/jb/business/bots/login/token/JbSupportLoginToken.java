package com.jb.business.bots.login.token;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

public class JbSupportLoginToken implements CcpBusiness{

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		JbSupportLoginTokenTypes ticketType = json.getAsEnum(JsonFields.ticketType, JbSupportLoginTokenTypes.class);
		CcpJsonRepresentation execute = ticketType.execute(json);
		return execute;
	}
	
	public Class<?> getJsonValidationClass() {
		return JsonFields.class;
	}
	
	
	public static enum JsonFields implements CcpJsonFieldName{
		
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = JbSupportLoginTokenTypes.class)
		ticketType
	}

}
