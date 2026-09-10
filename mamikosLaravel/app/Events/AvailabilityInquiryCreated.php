<?php

declare(strict_types=1);

namespace App\Events;

use App\Models\AvailabilityInquiry;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

/**
 * Published right after an inquiry is committed (US-08). Kept decoupled
 * from AvailabilityInquiryService so notification concerns never touch the
 * credit-deduction transaction.
 */
class AvailabilityInquiryCreated
{
    use Dispatchable, SerializesModels;

    public function __construct(public readonly AvailabilityInquiry $inquiry) {}
}
