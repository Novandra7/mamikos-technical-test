<?php

declare(strict_types=1);

namespace Tests\Unit\Services;

use App\Exceptions\Domain\InquiryAlreadyAnsweredException;
use App\Exceptions\Domain\NotKostOwnerException;
use App\Models\AvailabilityInquiry;
use App\Models\Kost;
use App\Models\User;
use App\Services\AvailabilityInquiryService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

class AvailabilityInquiryServiceTest extends TestCase
{
    use CreatesUsers, RefreshDatabase;

    private AvailabilityInquiryService $service;

    protected function setUp(): void
    {
        parent::setUp();

        $this->service = app(AvailabilityInquiryService::class);
    }

    public function test_create_snapshots_available_rooms_at_the_moment_of_inquiry(): void
    {
        $kost = Kost::factory()->create(['available_rooms' => 7, 'total_rooms' => 10]);
        $user = $this->createRegular(balance: 20);

        $result = $this->service->create($kost->id, $user->id, 'ada kamar?');

        $this->assertSame(7, $result['inquiry']->available_rooms_snapshot);
        $this->assertSame(5, $result['charged']);
        $this->assertSame(15, $result['balance_after']);
    }

    public function test_reply_by_a_different_owner_is_rejected(): void
    {
        $kost = Kost::factory()->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->create();
        $intruder = User::factory()->owner()->create();

        $this->expectException(NotKostOwnerException::class);

        $this->service->reply($inquiry->id, $intruder->id, 'balasan');
    }

    public function test_replying_to_an_already_answered_inquiry_is_rejected(): void
    {
        $kost = Kost::factory()->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->answered()->create();

        $this->expectException(InquiryAlreadyAnsweredException::class);

        $this->service->reply($inquiry->id, $kost->owner_id, 'balasan lagi');
    }
}
