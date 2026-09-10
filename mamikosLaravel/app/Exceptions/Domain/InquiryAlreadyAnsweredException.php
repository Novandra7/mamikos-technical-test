<?php

declare(strict_types=1);

namespace App\Exceptions\Domain;

final class InquiryAlreadyAnsweredException extends DomainException
{
    public function __construct()
    {
        parent::__construct('This inquiry has already been answered.');
    }

    public function httpStatus(): int
    {
        return 409;
    }

    public function errorCode(): string
    {
        return 'INQUIRY_ALREADY_ANSWERED';
    }
}
