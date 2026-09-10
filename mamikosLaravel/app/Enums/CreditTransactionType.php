<?php

declare(strict_types=1);

namespace App\Enums;

/**
 * Every balance mutation is tagged with one of these types so the ledger
 * (credit_transactions) stays a meaningful audit trail (BR-04).
 */
enum CreditTransactionType: string
{
    case INITIAL_GRANT = 'INITIAL_GRANT';
    case MONTHLY_RECHARGE = 'MONTHLY_RECHARGE';
    case INQUIRY_DEDUCTION = 'INQUIRY_DEDUCTION';
    case ADJUSTMENT = 'ADJUSTMENT';
}
