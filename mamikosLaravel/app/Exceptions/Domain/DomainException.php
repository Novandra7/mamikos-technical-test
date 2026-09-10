<?php

declare(strict_types=1);

namespace App\Exceptions\Domain;

use RuntimeException;

/**
 * Base type for every business-rule violation. The exception handler
 * (bootstrap/app.php) maps any DomainException to the standard error
 * envelope using httpStatus()/errorCode()/errors() below, so a service can
 * simply `throw` instead of building an HTTP response itself.
 */
abstract class DomainException extends RuntimeException
{
    abstract public function httpStatus(): int;

    abstract public function errorCode(): string;

    /**
     * @return array<string, list<string>>
     */
    public function errors(): array
    {
        return [];
    }
}
