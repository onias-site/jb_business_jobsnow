package com.jb.business.bots.engine;

import java.util.function.Supplier;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

public enum JbBotType implements CcpJsonFieldName{
	user {
		@Override
		protected boolean isRestricted() {
			return false;
		}
	}, support {
		protected boolean isRestricted() {
			return true;
		}
	};
	
	Supplier<CcpJsonRepresentation> getParameterToSearchBot() {
		String botName =  this.name();
		var parameterToSearchBots = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonInstantMessengerFields.botName, botName)
		;
		Supplier<CcpJsonRepresentation> jsonSupplier = parameterToSearchBots.getJsonSupplier();
		return jsonSupplier;
	}
	
	public Bot getBot() {
		Bot bot = JbBotEngine.INSTANCE.allBots.get(this);
		return bot;
	}
	
	abstract protected boolean isRestricted();
}
