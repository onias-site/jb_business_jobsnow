package com.jb.business.bots.engine;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

enum StepFields implements CcpJsonFieldName{
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeString
	typedValue
	;
	
}
