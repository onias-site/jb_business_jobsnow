package com.jb.business.bots.login.token;

import java.util.List;
import java.util.function.Predicate;

import com.ccp.decorators.CcpJsonRepresentation;
import static com.jb.business.bots.login.token.LoginTokenTicketsJsonTransformers.*;
public enum LoginTokenTicketsJsonConditions implements Predicate<CcpJsonRepresentation>{

	ifIsTheLastLoginTokenTicket{

		@Override
		public boolean test(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = json.whenAllFieldsAreFound(readAllLoginTokenTicketsFunction, LoginTokenTicketsJsonFields.values());
			List<CcpJsonRepresentation> listValues = result.getAsJsonList(LoginTokenTicketsJsonFields.listValues);
			Integer counter = result.getAsIntegerNumber(LoginTokenTicketsJsonFields.counter);
			int position = counter + 1;

			boolean isTheLastLoginTokenTicket = position == listValues.size();
			
			return isTheLastLoginTokenTicket;
		}
		
	}
	
	
}
