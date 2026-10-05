package com.jb.business.bots.engine;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

/** Input rules of a step. */
enum StepFields implements CcpJsonFieldName{
	/** The {@code typedValue} field: required, text. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeString
	typedValue
	;
	
}
