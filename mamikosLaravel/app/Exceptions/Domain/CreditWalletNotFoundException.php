<?php

declare(strict_types=1);

namespace App\Exceptions\Domain;

/**
 * D-02: raised when credit logic is (incorrectly) invoked for a user who
 * has no wallet at all — normally an OWNER. This is a defensive invariant:
 * role middleware/policies should already have blocked the request before
 * it ever reaches the credit service.
 */
final class CreditWalletNotFoundException extends DomainException
{
    public function __construct()
    {
        parent::__construct('This account does not have a credit wallet.');
    }

    public function httpStatus(): int
    {
        return 403;
    }

    public function errorCode(): string
    {
        return 'FORBIDDEN';
    }
}
