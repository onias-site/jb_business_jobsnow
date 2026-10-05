package com.jb.business.bots.login.token;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.process.CcpProcessStatusDefault;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jn.business.login.solve.token.JnBusinessResetLoginToken;
import com.jn.business.messages.JnMessages.JnNotifySupportAboutPendingLockedLoginToken;
import com.jn.business.messages.JnMessages.JnNotifySupportAboutPendingResendLoginToken;
import com.jn.entities.JnEntityInstantMessengerTemplateMessage;
import com.jn.entities.JnEntityLoginToken;
import com.jn.entities.JnEntityLoginTokenRequestResend;
import com.jn.entities.JnEntityLoginTokenRequestUnlock;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;

/**
 * The types of login token tickets: each one names the notice sent to the support team when the ticket is opened and
 * the entity that holds the open tickets.
 */
public enum JbSupportLoginTokenTypes implements CcpBusiness{
	/** A request to resend the login token. */
	resendToken(JnNotifySupportAboutPendingResendLoginToken.class, JnEntityLoginTokenRequestResend.ENTITY), 
	/** A request to unlock the login token. */
	unlockToken(JnNotifySupportAboutPendingLockedLoginToken.class, JnEntityLoginTokenRequestUnlock.ENTITY)
	;
	/** The notice sent to the support team when the ticket is opened (its class name is the template id). */
	public final Class<?> sender;
	/** The entity of the open tickets; the twin holds the solved ones. */
	public final CcpEntity entity;


	/**
	 * Associates the type with its notice and entity.
	 * @param sender the notice
	 * @param entity the entity of the open tickets
	 */
	private JbSupportLoginTokenTypes(Class<?> sender, CcpEntity entity) {
		this.sender = sender;
		this.entity = entity;
	}
	/**
	 * Solves the ticket: draws a new token, sends it by e-mail to the user and moves the ticket to its twin, which notifies
	 * the support team with that same token.
	 * <p>The token is drawn here, and not left to the transformer the saving would trigger, because the saving only returns
	 * whether there was an insertion: the value drawn in there would not come back, and the message to the support team must
	 * carry exactly the token the user received by e-mail.
	 * <p>The reset comes before the saving to clean the previous token (which may be locked, that is, in the twin of
	 * {@code login_token}) and to delete the record that marks the token e-mail as already sent, which would refuse the
	 * resending as a repetition and interrupt the ticket.
	 * <p>Nothing is done when there is no open ticket for the e-mail: the {@code NOT_FOUND} status is what the
	 * {@code solveLoginTokenTicket} step has configured in its {@code stepFlow} to tell the support team that the e-mail did
	 * not ask for this kind of help. Without this refusal, a mistyped e-mail would replace the token of someone who asked for
	 * nothing.
	 * @param json the session, with {@code email}
	 * @return the session with the new token
	 * @throws CcpErrorFlowDisturb with {@code NOT_FOUND} when there is no open ticket
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		boolean thereIsNoOpenTicket = false == this.entity.exists(json);

		if(thereIsNoOpenTicket) {
			CcpProcessStatusDefault.NOT_FOUND.throwException(json);
		}

		CcpJsonRepresentation resetedToken = JnBusinessResetLoginToken.INSTANCE.execute(json);

		String newToken = JnJsonTransformersFieldsEntityDefault.getOriginalToken();

		CcpJsonRepresentation withTheNewToken = resetedToken.put(JnJsonCommonsFields.originalToken, newToken);

		JnEntityLoginToken.ENTITY.save(withTheNewToken);

		CcpJsonRepresentation withTheTokenToTheSupport = withTheNewToken.put(JnEntityLoginToken.Fields.token, newToken);

		this.entity.delete(withTheTokenToTheSupport);

		return withTheTokenToTheSupport;
	}
	/**
	 * Seeds the template of the notice to the support team: the {@code solveLoginTokenTicket} command of this type for the
	 * e-mail (Portuguese only).
	 * @return the seed records
	 */
	public List<CcpBulkItem> getInstantMessageTemplate(){
		Class<?> class1 = this.sender;
		String templateId = class1.getName();
		CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId)
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese)
				.put(JnJsonCommonsFields.message, "/" + JbSupportBotCommands.solveLoginTokenTicket + " " + this.name() + " {email} ")
				;
		
		List<CcpBulkItem> bulkItems = JnEntityInstantMessengerTemplateMessage.ENTITY.toBulkItems(json, CcpBulkEntityOperationType.create);

		return bulkItems;
	}
}
