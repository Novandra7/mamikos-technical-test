<?php

declare(strict_types=1);

namespace App\Models;

use Database\Factories\CreditBalanceFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Carbon;

/**
 * @property int $id
 * @property int $user_id
 * @property int $balance
 * @property int $version
 * @property Carbon|null $last_recharged_at
 * @property-read User $user
 */
class CreditBalance extends Model
{
    /** @use HasFactory<CreditBalanceFactory> */
    use HasFactory;

    protected $fillable = [
        'user_id',
        'balance',
        'version',
        'last_recharged_at',
    ];

    protected function casts(): array
    {
        return [
            'balance' => 'integer',
            'version' => 'integer',
            'last_recharged_at' => 'datetime',
        ];
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
