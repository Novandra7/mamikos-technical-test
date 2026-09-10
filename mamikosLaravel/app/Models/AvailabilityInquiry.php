<?php

declare(strict_types=1);

namespace App\Models;

use App\Enums\InquiryStatus;
use Database\Factories\AvailabilityInquiryFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Carbon;

/**
 * @property int $id
 * @property int $kost_id
 * @property int $user_id
 * @property string|null $message
 * @property int $credit_charged
 * @property int $available_rooms_snapshot
 * @property InquiryStatus $status
 * @property string|null $owner_reply
 * @property Carbon|null $replied_at
 * @property Carbon|null $created_at
 * @property Carbon|null $updated_at
 * @property-read Kost $kost
 * @property-read User $user
 */
class AvailabilityInquiry extends Model
{
    /** @use HasFactory<AvailabilityInquiryFactory> */
    use HasFactory;

    protected $fillable = [
        'kost_id',
        'user_id',
        'message',
        'credit_charged',
        'available_rooms_snapshot',
        'status',
        'owner_reply',
        'replied_at',
    ];

    protected function casts(): array
    {
        return [
            'status' => InquiryStatus::class,
            'credit_charged' => 'integer',
            'available_rooms_snapshot' => 'integer',
            'replied_at' => 'datetime',
        ];
    }

    /**
     * @return BelongsTo<Kost, $this>
     */
    public function kost(): BelongsTo
    {
        return $this->belongsTo(Kost::class);
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
