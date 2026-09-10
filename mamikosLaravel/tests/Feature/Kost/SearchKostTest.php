<?php

declare(strict_types=1);

namespace Tests\Feature\Kost;

use App\Models\Kost;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SearchKostTest extends TestCase
{
    use RefreshDatabase;

    public function test_search_without_filters_returns_only_active_paginated_kosts(): void
    {
        Kost::factory()->count(15)->create();

        $response = $this->getJson('/api/v1/kosts');

        $response->assertOk()->assertJsonCount(10, 'data');
        $this->assertSame(15, $response->json('meta.pagination.total'));
    }

    public function test_filter_by_name_is_partial_and_case_insensitive(): void
    {
        Kost::factory()->create(['name' => 'Kost Melati Residence']);
        Kost::factory()->create(['name' => 'Kost Mawar']);

        $response = $this->getJson('/api/v1/kosts?name=melati');

        $response->assertOk()->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.name', 'Kost Melati Residence');
    }

    public function test_filter_by_location_matches_city(): void
    {
        Kost::factory()->create(['address_city' => 'Sleman']);
        Kost::factory()->create(['address_city' => 'Bandung']);

        $response = $this->getJson('/api/v1/kosts?location=Sleman');

        $response->assertOk()->assertJsonCount(1, 'data');
    }

    public function test_filter_by_location_matches_district(): void
    {
        Kost::factory()->create(['address_district' => 'Depok', 'address_city' => 'Sleman']);
        Kost::factory()->create(['address_district' => 'Coblong', 'address_city' => 'Bandung']);

        $response = $this->getJson('/api/v1/kosts?location=Depok');

        $response->assertOk()->assertJsonCount(1, 'data');
    }

    public function test_price_range_is_inclusive_on_both_ends(): void
    {
        Kost::factory()->create(['price_per_month' => 800000]);
        Kost::factory()->create(['price_per_month' => 1500000]);
        Kost::factory()->create(['price_per_month' => 2500000]);

        $response = $this->getJson('/api/v1/kosts?price_min=800000&price_max=1500000');

        $response->assertOk()->assertJsonCount(2, 'data');
    }

    public function test_sort_by_price_ascending(): void
    {
        Kost::factory()->create(['price_per_month' => 2000000]);
        Kost::factory()->create(['price_per_month' => 500000]);

        $response = $this->getJson('/api/v1/kosts?sort_by=price&order=asc');

        $response->assertOk();
        $this->assertSame('500000.00', $response->json('data.0.price_per_month'));
    }

    public function test_sort_by_price_descending(): void
    {
        Kost::factory()->create(['price_per_month' => 2000000]);
        Kost::factory()->create(['price_per_month' => 500000]);

        $response = $this->getJson('/api/v1/kosts?sort_by=price&order=desc');

        $response->assertOk();
        $this->assertSame('2000000.00', $response->json('data.0.price_per_month'));
    }

    public function test_unknown_sort_column_is_rejected(): void
    {
        Kost::factory()->create();

        $this->getJson('/api/v1/kosts?sort_by=owner_id')->assertStatus(422)->assertJsonPath('code', 'VALIDATION_ERROR');
    }

    public function test_per_page_above_fifty_is_rejected(): void
    {
        $this->getJson('/api/v1/kosts?per_page=1000')->assertStatus(422);
    }

    public function test_inactive_kosts_are_excluded(): void
    {
        Kost::factory()->inactive()->create();

        $this->getJson('/api/v1/kosts')->assertOk()->assertJsonCount(0, 'data');
    }

    public function test_soft_deleted_kosts_are_excluded(): void
    {
        $kost = Kost::factory()->create();
        $kost->delete();

        $this->getJson('/api/v1/kosts')->assertOk()->assertJsonCount(0, 'data');
    }

    public function test_available_rooms_is_never_present_in_search_results(): void
    {
        Kost::factory()->create();

        $this->getJson('/api/v1/kosts')->assertOk()->assertJsonMissingPath('data.0.available_rooms');
    }

    public function test_guest_can_search_without_authentication(): void
    {
        Kost::factory()->create();

        $this->getJson('/api/v1/kosts')->assertOk();
    }
}
