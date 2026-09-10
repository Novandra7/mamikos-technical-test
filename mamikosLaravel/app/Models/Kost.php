<?php

declare(strict_types=1);

namespace App\Models;

use App\Enums\RoomType;
use Database\Factories\KostFactory;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Collection;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Carbon;

/**
 * @property int $id
 * @property int $owner_id
 * @property string $name
 * @property string|null $description
 * @property string $address_street
 * @property string $address_district
 * @property string $address_city
 * @property string $address_province
 * @property string|null $postal_code
 * @property string|null $latitude
 * @property string|null $longitude
 * @property string $price_per_month
 * @property RoomType $room_type
 * @property int $total_rooms
 * @property int $available_rooms
 * @property bool $is_active
 * @property Carbon|null $created_at
 * @property Carbon|null $updated_at
 * @property Carbon|null $deleted_at
 * @property-read User $owner
 * @property-read Collection<int, KostFacility> $facilities
 * @property-read Collection<int, KostPhoto> $photos
 */
class Kost extends Model
{
    /** @use HasFactory<KostFactory> */
    use HasFactory, SoftDeletes;

    protected $fillable = [
        'owner_id',
        'name',
        'description',
        'address_street',
        'address_district',
        'address_city',
        'address_province',
        'postal_code',
        'latitude',
        'longitude',
        'price_per_month',
        'room_type',
        'total_rooms',
        'available_rooms',
        'is_active',
    ];

    protected function casts(): array
    {
        return [
            'price_per_month' => 'decimal:2',
            'latitude' => 'decimal:8',
            'longitude' => 'decimal:8',
            'room_type' => RoomType::class,
            'total_rooms' => 'integer',
            'available_rooms' => 'integer',
            'is_active' => 'boolean',
        ];
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function owner(): BelongsTo
    {
        return $this->belongsTo(User::class, 'owner_id');
    }

    /**
     * @return HasMany<KostFacility, $this>
     */
    public function facilities(): HasMany
    {
        return $this->hasMany(KostFacility::class);
    }

    /**
     * @return HasMany<KostPhoto, $this>
     */
    public function photos(): HasMany
    {
        return $this->hasMany(KostPhoto::class)->orderBy('sort_order');
    }

    /**
     * @return HasMany<AvailabilityInquiry, $this>
     */
    public function inquiries(): HasMany
    {
        return $this->hasMany(AvailabilityInquiry::class);
    }

    /**
     * Only kosts that are allowed to appear in public search/detail (BR-07).
     *
     * @param  Builder<Kost>  $query
     * @return Builder<Kost>
     */
    public function scopePubliclyVisible(Builder $query): Builder
    {
        return $query->where('is_active', true);
    }
}
