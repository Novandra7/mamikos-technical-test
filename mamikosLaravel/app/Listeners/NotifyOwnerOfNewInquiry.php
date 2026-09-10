<?php

declare(strict_types=1);

namespace App\Listeners;

use App\Events\AvailabilityInquiryCreated;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * O6/REL-04: email/SMS delivery is out of scope, but the side effect must
 * be ready to extend — so it is queued, retried, and logged in a
 * structured way an actual notification channel could plug into later.
 */
class NotifyOwnerOfNewInquiry implements ShouldQueue
{
    public int $tries = 3;

    /**
     * @return list<int>
     */
    public function backoff(): array
    {
        return [10, 30, 60];
    }

    public function handle(AvailabilityInquiryCreated $event): void
    {
        $inquiry = $event->inquiry->loadMissing('kost.owner');

        Log::info('inquiry.owner_notified', [
            'inquiry_id' => $inquiry->id,
            'kost_id' => $inquiry->kost_id,
            'owner_id' => $inquiry->kost->owner_id,
            'owner_email' => $inquiry->kost->owner->email,
            'user_id' => $inquiry->user_id,
        ]);
    }
}
