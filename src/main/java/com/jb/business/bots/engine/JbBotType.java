package com.jb.business.bots.engine;

import java.util.function.Supplier;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/** The bots of the platform. */
public enum JbBotType implements CcpJsonFieldName{
	/** The bot that talks to the users; open to everybody. */
	user {
		/**
		 * Open to everybody.
		 * @return {@code false}
		 */
		@Override
		protected boolean isRestricted() {
			return false;
		}
	},
	/** The support bot; restricted to the allowed users. */
	support {
		/**
		 * Restricted to the allowed users.
		 * @return {@code true}
		 */
		protected boolean isRestricted() {
			return true;
		}
	};
	
	/**
	 * Returns the key to search the records of the bot.
	 * @return a supplier of {@code botName}
	 */
	Supplier<CcpJsonRepresentation> getParameterToSearchBot() {
		String botName =  this.name();
		var parameterToSearchBots = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonInstantMessengerFields.botName, botName)
		;
		Supplier<CcpJsonRepresentation> jsonSupplier = parameterToSearchBots.getJsonSupplier();
		return jsonSupplier;
	}
	
	/**
	 * Returns the bot loaded by the engine.
	 * @return the bot
	 */
	public Bot getBot() {
		Bot bot = JbBotEngine.INSTANCE.allBots.get(this);
		return bot;
	}
	
	/**
	 * Tells whether only the allowed users may use the bot.
	 * @return {@code true} for a restricted bot
	 */
	abstract protected boolean isRestricted();
}
