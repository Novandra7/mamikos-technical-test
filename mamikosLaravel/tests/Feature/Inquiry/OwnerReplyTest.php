<?php

declare(strict_types=1);

namespace Tests\Feature\Inquiry;

use App\Models\AvailabilityInquiry;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class OwnerReplyTest extends TestCase
{
    use RefreshDatabase;

    public function test_owner_sees_inquiries_across_all_their_kosts(): void
    {
        $owner = User::factory()->owner()->create();
        $kostA = Kost::factory()->forOwner($owner)->create();
        $kostB = Kost::factory()->forOwner($owner)->create();
        AvailabilityInquiry::factory()->for($kostA)->create();
        AvailabilityInquiry::factory()->for($kostB)->create();
        AvailabilityInquiry::factory()->for(Kost::factory()->create())->create();

        Sanctum::actingAs($owner, ['*']);

        $this->getJson('/api/v1/owner/inquiries')->assertOk()->assertJsonCount(2, 'data');
    }

    public function test_owner_can_reply_to_a_pending_inquiry(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->create();

        Sanctum::actingAs($owner, ['*']);

        $response = $this->postJson("/api/v1/owner/inquiries/{$inquiry->id}/reply", ['reply' => 'Masih ada 3 kamar.']);

        $response->assertOk()->assertJsonPath('data.status', 'ANSWERED');
        $this->assertDatabaseHas('availability_inquiries', ['id' => $inquiry->id, 'owner_reply' => 'Masih ada 3 kamar.']);
    }

    public function test_replying_to_another_owners_inquiry_is_forbidden(): void
    {
        $ownerA = User::factory()->owner()->create();
        $ownerB = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($ownerB)->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->create();

        Sanctum::actingAs($ownerA, ['*']);

        $this->postJson("/api/v1/owner/inquiries/{$inquiry->id}/reply", ['reply' => 'tes'])
            ->assertStatus(403)->assertJsonPath('code', 'NOT_KOST_OWNER');
    }

    public function test_replying_twice_is_rejected(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->answered()->create();

        Sanctum::actingAs($owner, ['*']);

        $this->postJson("/api/v1/owner/inquiries/{$inquiry->id}/reply", ['reply' => 'lagi'])
            ->assertStatus(409)->assertJsonPath('code', 'INQUIRY_ALREADY_ANSWERED');
    }
}
