<?php

declare(strict_types=1);

namespace App\Exceptions\Domain;

/**
 * Deliberately generic message — it must not leak whether the email is
 * registered (US-02).
 */
final class InvalidCredentialsException extends DomainException
{
    public function __construct()
    {
        parent::__construct('The provided credentials are incorrect.');
    }

    public function httpStatus(): int
    {
        return 401;
    }

    public function errorCode(): string
    {
        return 'INVALID_CREDENTIALS';
    }
}
