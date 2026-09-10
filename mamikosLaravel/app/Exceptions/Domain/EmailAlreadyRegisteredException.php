<?php

declare(strict_types=1);

namespace App\Exceptions\Domain;

final class EmailAlreadyRegisteredException extends DomainException
{
    public function __construct()
    {
        parent::__construct('This email address is already registered.');
    }

    public function httpStatus(): int
    {
        return 409;
    }

    public function errorCode(): string
    {
        return 'EMAIL_ALREADY_REGISTERED';
    }
}
