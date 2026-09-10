<?php

declare(strict_types=1);

namespace Tests\Feature\Owner;

use App\Models\Kost;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class UpdateKostTest extends TestCase
{
    use RefreshDatabase;

    public function test_owner_can_partially_update_their_own_kost(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create(['price_per_month' => 950000, 'available_rooms' => 4, 'total_rooms' => 12]);

        Sanctum::actingAs($owner, ['*']);

        $response = $this->patchJson("/api/v1/owner/kosts/{$kost->id}", [
            'price_per_month' => 1100000,
            'available_rooms' => 3,
        ]);

        $response->assertOk()->assertJsonPath('data.price_per_month', '1100000.00');
        $this->assertDatabaseHas('kosts', ['id' => $kost->id, 'available_rooms' => 3]);
    }

    public function test_owner_a_cannot_update_owner_bs_kost(): void
    {
        $ownerA = User::factory()->owner()->create();
        $ownerB = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($ownerB)->create();

        Sanctum::actingAs($ownerA, ['*']);

        $response = $this->patchJson("/api/v1/owner/kosts/{$kost->id}", ['price_per_month' => 1000000]);

        $response->assertStatus(403)->assertJsonPath('code', 'NOT_KOST_OWNER');
    }

    public function test_available_rooms_cannot_exceed_total_rooms_after_update(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create(['total_rooms' => 5, 'available_rooms' => 2]);

        Sanctum::actingAs($owner, ['*']);

        $response = $this->patchJson("/api/v1/owner/kosts/{$kost->id}", ['available_rooms' => 10]);

        $response->assertStatus(422)->assertJsonPath('code', 'VALIDATION_ERROR');
    }

    public function test_full_update_via_put_replaces_the_kost(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();

        Sanctum::actingAs($owner, ['*']);

        $response = $this->putJson("/api/v1/owner/kosts/{$kost->id}", [
            'name' => 'Kost Baru',
            'description' => 'Deskripsi baru',
            'address' => ['street' => 'Jl. Baru', 'district' => 'Baru', 'city' => 'Baru', 'province' => 'Baru'],
            'price_per_month' => 800000,
            'room_type' => 'campur',
            'total_rooms' => 8,
            'available_rooms' => 2,
        ]);

        $response->assertOk()->assertJsonPath('data.name', 'Kost Baru');
    }
}
