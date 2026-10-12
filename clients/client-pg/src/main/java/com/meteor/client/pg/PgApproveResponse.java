package com.meteor.client.pg;

record PgApproveResponse(String transactionKey) {

    PgApproval toApproval() {
        return new PgApproval(transactionKey);
    }

}
