<?php

declare(strict_types=1);

namespace Tests\Feature\Kost;

use App\Models\Kost;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class KostDetailTest extends TestCase
{
    use RefreshDatabase;

    public function test_guest_can_view_kost_detail_without_available_rooms(): void
    {
        $kost = Kost::factory()->create();

        $response = $this->getJson('/api/v1/kosts/'.$kost->id);

        $response->assertOk()
            ->assertJsonPath('data.availability.disclosed', false)
            ->assertJsonMissingPath('data.available_rooms');
    }

    public function test_inactive_kost_returns_not_found(): void
    {
        $kost = Kost::factory()->inactive()->create();

        $this->getJson('/api/v1/kosts/'.$kost->id)->assertStatus(404)->assertJsonPath('code', 'RESOURCE_NOT_FOUND');
    }

    public function test_nonexistent_kost_returns_not_found(): void
    {
        $this->getJson('/api/v1/kosts/999999')->assertStatus(404);
    }

    public function test_owner_phone_is_hidden_from_guests(): void
    {
        $owner = User::factory()->owner()->create(['phone' => '081200000099']);
        $kost = Kost::factory()->forOwner($owner)->create();

        $this->getJson('/api/v1/kosts/'.$kost->id)->assertJsonPath('data.owner.phone', null);
    }

    public function test_owner_phone_is_visible_to_authenticated_users(): void
    {
        $owner = User::factory()->owner()->create(['phone' => '081200000099']);
        $kost = Kost::factory()->forOwner($owner)->create();

        Sanctum::actingAs(User::factory()->regular()->create(), ['*']);

        $this->getJson('/api/v1/kosts/'.$kost->id)->assertJsonPath('data.owner.phone', '081200000099');
    }
}
