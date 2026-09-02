package com.jb.business.bots.engine;

import java.util.function.Predicate;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.jn.json.fields.validation.JnJsonCommonsFields;

enum JsonConditions implements Predicate<CcpJsonRepresentation>{
	IfFieldExists{

		public boolean test(CcpJsonRepresentation json) {
			boolean containsAllFields = json.containsAllFields(jsonFieldName);
			return containsAllFields;
		}
	},
	thisFieldIsValidJson{

		public boolean test(CcpJsonRepresentation json) {
			CcpStringDecorator asStringDecorator = json.getAsStringDecorator(jsonFieldName);
			boolean innerJson = asStringDecorator.isInnerJson();
			return innerJson;
		}
	}
	;
	static final CcpJsonFieldName jsonFieldName = JnJsonCommonsFields.json;

}
