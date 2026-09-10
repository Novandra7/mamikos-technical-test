<?php

declare(strict_types=1);

namespace App\Services;

use App\Enums\InquiryStatus;
use App\Events\AvailabilityInquiryCreated;
use App\Exceptions\Domain\InquiryAlreadyAnsweredException;
use App\Exceptions\Domain\NotKostOwnerException;
use App\Models\AvailabilityInquiry;
use App\Repositories\Contracts\InquiryRepositoryInterface;
use App\Repositories\Contracts\KostRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

/**
 * Implements the "ask about room availability" flow (US-08) and the
 * owner-side reply flow (US-10). See PRD §13.3 for the exact request
 * pipeline this mirrors.
 */
final class AvailabilityInquiryService
{
    public function __construct(
        private readonly KostRepositoryInterface $kosts,
        private readonly InquiryRepositoryInterface $inquiries,
        private readonly CreditService $creditService,
    ) {}

    /**
     * @return array{inquiry: AvailabilityInquiry, balance_before: int, balance_after: int, charged: int}
     */
    public function create(int $kostId, int $userId, ?string $message): array
    {
        // Step 1: 404 if the kost does not exist, is inactive, or is
        // soft-deleted — resolved *before* touching the wallet so an
        // invalid kost id never has a side effect on credit.
        $kost = $this->kosts->findActiveOrFail($kostId);

        $cost = (int) config('credit.inquiry_cost', 5);

        $result = $this->creditService->deductWithReference(
            userId: $userId,
            amount: $cost,
            description: "Availability inquiry for kost #{$kost->id}",
            referenceType: 'availability_inquiry',
            createReference: fn () => $this->inquiries->create([
                'kost_id' => $kost->id,
                'user_id' => $userId,
                'message' => $message,
                'credit_charged' => $cost,
                'available_rooms_snapshot' => $kost->available_rooms,
                'status' => InquiryStatus::PENDING,
            ]),
        );

        /** @var AvailabilityInquiry $inquiry */
        $inquiry = $result['reference'];

        event(new AvailabilityInquiryCreated($inquiry));

        return [
            'inquiry' => $inquiry->fresh('kost'),
            'balance_before' => $result['balance']->balance + $cost,
            'balance_after' => $result['balance']->balance,
            'charged' => $cost,
        ];
    }

    /**
     * @return LengthAwarePaginator<int, AvailabilityInquiry>
     */
    public function myInquiries(int $userId, int $page, int $perPage): LengthAwarePaginator
    {
        return $this->inquiries->paginateForUser($userId, $page, $perPage);
    }

    /**
     * @param  array<string, mixed>  $filters
     * @return LengthAwarePaginator<int, AvailabilityInquiry>
     */
    public function forOwner(int $ownerId, array $filters, int $page, int $perPage): LengthAwarePaginator
    {
        return $this->inquiries->paginateForOwner($ownerId, $filters, $page, $perPage);
    }

    /**
     * @throws NotKostOwnerException
     * @throws InquiryAlreadyAnsweredException
     */
    public function reply(int $inquiryId, int $ownerId, string $reply): AvailabilityInquiry
    {
        $inquiry = $this->inquiries->findOrFail($inquiryId);

        if ($inquiry->kost->owner_id !== $ownerId) {
            throw new NotKostOwnerException('You are not the owner of the kost this inquiry belongs to.');
        }

        if ($inquiry->status === InquiryStatus::ANSWERED) {
            throw new InquiryAlreadyAnsweredException;
        }

        $inquiry->update([
            'status' => InquiryStatus::ANSWERED,
            'owner_reply' => $reply,
            'replied_at' => now(),
        ]);

        return $inquiry->fresh(['kost', 'user']);
    }
}
