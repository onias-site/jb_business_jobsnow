package com.jb.business.bots.engine;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Action of {@code BotCommandStep.apply} when the command has a next step: saves the session at that step, keeping the
 * fields that never change during the command (bot, chat and command).
 */
class BotCommandStepUpdateSession implements CcpBusiness {

	/** The step whose session is saved. */
	private final BotCommandStep step;

	/** The fields that never change during the command. */
	private final CcpJsonRepresentation ummutableFields;

	/** The next step. */
	private final String nextStep;

	/**
	 * Keeps what the session receives.
	 * @param step the step whose session is saved
	 * @param ummutableFields the fields that never change during the command
	 * @param nextStep the next step
	 */
	BotCommandStepUpdateSession(BotCommandStep step, CcpJsonRepresentation ummutableFields, String nextStep) {
		this.step = step;
		this.ummutableFields = ummutableFields;
		this.nextStep = nextStep;
	}

	/**
	 * Saves the session at the next step.
	 * @param json the result of the step
	 * @return the saved session
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation jsonPreservingUmmatableFields = json.mergeWithAnotherJson(this.ummutableFields);
		CcpJsonRepresentation savedSession = this.step.saveSession(jsonPreservingUmmatableFields, this.nextStep);
		return savedSession;
	}
}
