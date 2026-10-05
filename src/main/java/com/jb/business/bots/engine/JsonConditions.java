package com.jb.business.bots.engine;

import java.util.function.Predicate;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/** Conditions on the {@code json} field of a session. */
enum JsonConditions implements Predicate<CcpJsonRepresentation>{
	/** The {@code json} field exists. */
	IfFieldExists{

		/**
		 * Tells whether the field exists.
		 * @param json the session
		 * @return {@code true} when it exists
		 */
		public boolean test(CcpJsonRepresentation json) {
			boolean containsAllFields = json.containsAllFields(jsonFieldName);
			return containsAllFields;
		}
	},
	/** The {@code json} field holds a valid JSON. */
	thisFieldIsValidJson{

		/**
		 * Tells whether the field holds a valid JSON.
		 * @param json the session
		 * @return {@code true} when it does
		 */
		public boolean test(CcpJsonRepresentation json) {
			CcpStringDecorator asStringDecorator = json.getAsStringDecorator(jsonFieldName);
			boolean innerJson = asStringDecorator.isInnerJson();
			return innerJson;
		}
	}
	;
	/** The {@code json} field. */
	static final CcpJsonFieldName jsonFieldName = JnJsonCommonsFields.json;

}
