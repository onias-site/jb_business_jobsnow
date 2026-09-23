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

public enum JbSupportLoginTokenTypes implements CcpBusiness{
	resendToken(JnNotifySupportAboutPendingResendLoginToken.class, JnEntityLoginTokenRequestResend.ENTITY), 
	unlockToken(JnNotifySupportAboutPendingLockedLoginToken.class, JnEntityLoginTokenRequestUnlock.ENTITY)
	;
	public final Class<?> sender;
	public final CcpEntity entity;


	private JbSupportLoginTokenTypes(Class<?> sender, CcpEntity entity) {
		this.sender = sender;
		this.entity = entity;
	}
	/**
	 * Atende ao ticket: sorteia um token novo, envia-o por email ao usuário e transfere o ticket para a
	 * sua entidade gêmea, o que por sua vez notifica o suporte com esse mesmo token.
	 *
	 * <p>O token é sorteado aqui, e não deixado a cargo do transformador que a gravação dispararia,
	 * porque a gravação devolve apenas se houve inclusão: o valor sorteado lá dentro não voltaria, e a
	 * mensagem ao suporte tem que trazer exatamente o token que o usuário recebeu por email.
	 *
	 * <p>O reset antecede a gravação para limpar o token anterior — que pode estar bloqueado, ou seja,
	 * na gêmea de {@code login_token} — e para apagar o registro que marca o email do token como já
	 * enviado, que recusaria o reenvio como repetição e interromperia o atendimento do ticket.
	 *
	 * <p>Nada é feito quando não há ticket aberto para o e-mail informado: o status {@code NOT_FOUND}
	 * é o que o passo {@code solveLoginTokenTicket} tem configurado no seu {@code stepFlow} para
	 * responder ao suporte que aquele e-mail não pediu este tipo de atendimento. Sem esta recusa um
	 * e-mail digitado errado trocaria o token de quem nada pediu.
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
