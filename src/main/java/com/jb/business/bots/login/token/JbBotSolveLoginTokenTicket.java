package com.jb.business.bots.login.token;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jb.business.bots.engine.JbBotEngine.JbBotBusiness;
import static com.jb.business.bots.login.token.LoginTokenTicketsJsonConditions.*;
import static com.jb.business.bots.login.token.LoginTokenTicketsJsonTransformers.*;

public class JbBotSolveLoginTokenTicket implements JbBotBusiness{
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation transformedJsonWhenAllConditionsMatch = json
				.getTransformedJsonExecutingIfAndElse(
						ifIsTheLastLoginTokenTicket, 
						throwNewHasNoMoreLoginTokenTicketsToSolve, 
						solveLoginTokenTicketsFunction
						);
		
		return transformedJsonWhenAllConditionsMatch;
	}

	public Class<?> getJsonValidationClass() {
		return StepFields.class;
	}
	
	public static enum StepFields implements CcpJsonFieldName{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString
		typedValue
		;
		
	}
}
