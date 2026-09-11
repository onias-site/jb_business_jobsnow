package com.jb.business.bots.login.token;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jb.business.bots.engine.JbSupportBotCommands;
import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnNotifySupportAboutPendingLockedLoginToken;
import com.jn.business.messages.JnNotifySupportAboutPendingResendLoginToken;
import com.jn.entities.JnEntityInstantMessengerTemplateMessage;
import com.jn.entities.JnEntityLoginTokenRequestResend;
import com.jn.entities.JnEntityLoginTokenRequestUnlock;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;

public enum JbSupportLoginTokenTypes implements CcpBusiness{
	resendToken(JnNotifySupportAboutPendingResendLoginToken.INSTANCE, JnEntityLoginTokenRequestResend.ENTITY), 
	unlockToken(JnNotifySupportAboutPendingLockedLoginToken.INSTANCE, JnEntityLoginTokenRequestUnlock.ENTITY)
	;
	public final JnBusinessSendMessage sender;
	public final CcpEntity entity;


	private JbSupportLoginTokenTypes(JnBusinessSendMessage sender, CcpEntity entity) {
		this.sender = sender;
		this.entity = entity;
	}
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		this.entity.delete(json);
		return json;
	}
	public List<CcpBulkItem> getInstantMessageTemplate(){
		Class<? extends JnBusinessSendMessage> class1 = this.sender.getClass();
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
