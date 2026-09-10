<?php

declare(strict_types=1);

namespace Tests\Feature\Owner;

use App\Models\AvailabilityInquiry;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ListOwnerKostTest extends TestCase
{
    use RefreshDatabase;

    public function test_owner_sees_only_their_own_kosts(): void
    {
        $ownerA = User::factory()->owner()->create();
        $ownerB = User::factory()->owner()->create();
        Kost::factory()->forOwner($ownerA)->count(3)->create();
        Kost::factory()->forOwner($ownerB)->count(2)->create();

        Sanctum::actingAs($ownerA, ['*']);

        $response = $this->getJson('/api/v1/owner/kosts');

        $response->assertOk();
        $this->assertCount(3, $response->json('data'));
    }

    public function test_regular_user_cannot_access_owner_kost_listing(): void
    {
        Sanctum::actingAs(User::factory()->regular()->create(), ['*']);

        $this->getJson('/api/v1/owner/kosts')->assertStatus(403)->assertJsonPath('code', 'FORBIDDEN');
    }

    public function test_soft_deleted_kosts_are_hidden_unless_include_deleted_is_set(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();
        $kost->delete();

        Sanctum::actingAs($owner, ['*']);

        $this->getJson('/api/v1/owner/kosts')->assertOk()->assertJsonCount(0, 'data');
        $this->getJson('/api/v1/owner/kosts?include_deleted=true')->assertOk()->assertJsonCount(1, 'data');
    }

    public function test_listing_includes_inquiries_count(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();
        AvailabilityInquiry::factory()->for($kost)->count(2)->create();

        Sanctum::actingAs($owner, ['*']);

        $this->getJson('/api/v1/owner/kosts')->assertOk()->assertJsonPath('data.0.inquiries_count', 2);
    }
}
