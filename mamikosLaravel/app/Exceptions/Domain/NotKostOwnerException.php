<?php

declare(strict_types=1);

namespace App\Exceptions\Domain;

/**
 * D-05: an owner tried to modify, delete, or reply to something that
 * belongs to a different owner. Deliberately 403 (not 404) inside the
 * already-authenticated /owner/* namespace.
 */
final class NotKostOwnerException extends DomainException
{
    public function __construct(string $message = 'You are not the owner of this resource.')
    {
        parent::__construct($message);
    }

    public function httpStatus(): int
    {
        return 403;
    }

    public function errorCode(): string
    {
        return 'NOT_KOST_OWNER';
    }
}
