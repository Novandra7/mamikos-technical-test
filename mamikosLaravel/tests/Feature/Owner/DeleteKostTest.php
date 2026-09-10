<?php

declare(strict_types=1);

namespace Tests\Feature\Owner;

use App\Models\AvailabilityInquiry;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class DeleteKostTest extends TestCase
{
    use RefreshDatabase;

    public function test_owner_can_soft_delete_their_own_kost(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();

        Sanctum::actingAs($owner, ['*']);

        $this->deleteJson("/api/v1/owner/kosts/{$kost->id}")->assertOk();

        $this->assertSoftDeleted('kosts', ['id' => $kost->id]);
    }

    public function test_owner_a_cannot_delete_owner_bs_kost(): void
    {
        $ownerA = User::factory()->owner()->create();
        $ownerB = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($ownerB)->create();

        Sanctum::actingAs($ownerA, ['*']);

        $this->deleteJson("/api/v1/owner/kosts/{$kost->id}")->assertStatus(403)->assertJsonPath('code', 'NOT_KOST_OWNER');
        $this->assertNotSoftDeleted('kosts', ['id' => $kost->id]);
    }

    public function test_deleted_kost_disappears_from_public_search_and_detail(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create(['name' => 'Kost Unik Sekali']);

        Sanctum::actingAs($owner, ['*']);
        $this->deleteJson("/api/v1/owner/kosts/{$kost->id}")->assertOk();

        $this->getJson('/api/v1/kosts/'.$kost->id)->assertStatus(404)->assertJsonPath('code', 'RESOURCE_NOT_FOUND');
        $this->getJson('/api/v1/kosts?name=Kost+Unik+Sekali')->assertOk()->assertJsonCount(0, 'data');
    }

    public function test_inquiry_history_survives_kost_deletion(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->create();

        Sanctum::actingAs($owner, ['*']);
        $this->deleteJson("/api/v1/owner/kosts/{$kost->id}")->assertOk();

        $this->assertDatabaseHas('availability_inquiries', ['id' => $inquiry->id]);
    }
}
