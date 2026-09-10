<?php

declare(strict_types=1);

namespace App\Enums;

enum InquiryStatus: string
{
    case PENDING = 'PENDING';
    case ANSWERED = 'ANSWERED';
}
