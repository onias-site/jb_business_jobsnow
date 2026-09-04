package com.jb.business.bots.login.token;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.JnEntityLoginTokenRequestResend;
import com.jn.entities.JnEntityLoginTokenRequestUnlock;

public enum JbSupportLoginTokenTypes implements CcpBusiness{
	resendToken{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			JnEntityLoginTokenRequestResend.ENTITY.delete(json);
			return json;
		}
		
	}, 
	unlockToken{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			JnEntityLoginTokenRequestUnlock.ENTITY.delete(json);
			return json;
		}
	}
	;
}
