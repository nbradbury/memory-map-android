package com.nbradbury.memorymap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nbradbury.memorymap.data.IndexState
import com.nbradbury.memorymap.data.PhotoIndex
import com.nbradbury.memorymap.domain.Frame
import com.nbradbury.memorymap.domain.MON
import com.nbradbury.memorymap.domain.MonthRange
import com.nbradbury.memorymap.domain.Place
import com.nbradbury.memorymap.domain.dateRange
import com.nbradbury.memorymap.domain.filterPlaces
import com.nbradbury.memorymap.domain.fmtDate
import com.nbradbury.memorymap.domain.fmtSpan
import com.nbradbury.memorymap.domain.fmtTime
import com.nbradbury.memorymap.domain.monthSpan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** Each new [id] asks the map to fit the current places once. */
data class FitRequest(val id: Int, val animate: Boolean)

/** The custom-range sheet's draft: tap a start month, then an end month. */
data class Picker(val from: YearMonth, val to: YearMonth?, val year: Int) {
    fun tap(ym: YearMonth): Picker = when {
        to != null -> copy(from = ym, to = null)
        from <= ym -> copy(to = ym)
        else -> copy(from = ym, to = from)
    }

    val range: MonthRange get() = MonthRange(from, to ?: from)
}

data class Selection(
    val frame: Frame = Frame.ThisYear,
    val custom: MonthRange,
    val picker: Picker? = null,
    val placeId: String? = null,
    val fit: FitRequest = FitRequest(0, false),
)

data class ChipUi(val frame: Frame, val label: String, val selected: Boolean)

data class MonthCellUi(
    val ym: YearMonth,
    val label: String,
    val endpoint: Boolean,
    val inRange: Boolean,
    val enabled: Boolean,
    val hasPhotos: Boolean,
)

data class PickerUi(
    val year: Int,
    val canPrev: Boolean,
    val canNext: Boolean,
    val months: List<MonthCellUi>,
    val draftLabel: String,
)

data class SlideUi(val uri: String, val date: String, val time: String)

data class CarouselUi(val placeId: String, val name: String, val subtitle: String, val slides: List<SlideUi>)

data class UiState(
    val needsPermission: Boolean,
    val partialAccess: Boolean,
    val chips: List<ChipUi>,
    val rangeDesc: String,
    val statLine: String,
    val places: List<Place>,
    val noPlaces: Boolean,
    val picker: PickerUi?,
    val carousel: CarouselUi?,
    val fit: FitRequest,
)

private val FRAME_LABELS = listOf(
    Frame.ThisMonth to "This Month",
    Frame.LastMonth to "Last Month",
    Frame.ThisYear to "This Year",
    Frame.LastYear to "Last Year",
    Frame.Custom to "Custom",
)

fun defaultSelection(today: LocalDate): Selection {
    val now = YearMonth.from(today)
    return Selection(custom = MonthRange(now.minusMonths(3), now))
}

fun buildUiState(index: IndexState, s: Selection, today: LocalDate): UiState {
    val ready = index as? IndexState.Ready
    val r = dateRange(s.frame, s.custom, today)
    val places = ready?.let { filterPlaces(it.places, r.from, r.to) }.orEmpty()
    val photos = places.sumOf { it.photos.size }
    val nf = NumberFormat.getIntegerInstance()

    val statLine = when (index) {
        IndexState.NeedsPermission -> "Photo access needed"
        is IndexState.Scanning ->
            if (index.total == 0) "Loading your places…"
            else "Scanning photos… ${nf.format(index.done)} of ${nf.format(index.total)}"
        is IndexState.Ready ->
            "${places.size} ${if (places.size == 1) "place" else "places"} · " +
                "${nf.format(photos)} ${if (photos == 1) "photo" else "photos"}"
    }

    val chips = FRAME_LABELS.map { (f, label) ->
        val on = s.frame == f
        ChipUi(f, if (f == Frame.Custom && on) monthSpan(s.custom) else label, on)
    }

    val thisMonth = YearMonth.from(today)
    val minYear = ready?.minYear?.coerceAtMost(today.year) ?: today.year
    val picker = s.picker?.let { p ->
        val lo = minOf(p.from, p.to ?: p.from)
        val hi = maxOf(p.from, p.to ?: p.from)
        PickerUi(
            year = p.year,
            canPrev = p.year > minYear,
            canNext = p.year < today.year,
            months = MON.mapIndexed { m, label ->
                val ym = YearMonth.of(p.year, m + 1)
                MonthCellUi(
                    ym = ym,
                    label = label,
                    endpoint = ym == lo || ym == hi,
                    inRange = ym > lo && ym < hi,
                    enabled = ym <= thisMonth && ym.year >= minYear,
                    hasPhotos = ready?.monthsWithPhotos?.contains(ym) == true,
                )
            },
            draftLabel = monthSpan(p.range),
        )
    }

    val carousel = s.placeId?.let { id -> places.find { it.id == id } }?.let { place ->
        val ph = place.photos
        val span = fmtSpan(ph.first().date, ph.last().date)
        CarouselUi(
            placeId = place.id,
            name = place.name,
            subtitle = if (place.country.isEmpty()) span else "${place.country} · $span",
            slides = ph.map { SlideUi(it.uri, fmtDate(it.date), fmtTime(it.date)) },
        )
    }

    return UiState(
        needsPermission = index == IndexState.NeedsPermission,
        partialAccess = ready?.partialAccess == true,
        chips = chips,
        rangeDesc = r.desc,
        statLine = statLine,
        places = places,
        noPlaces = ready != null && places.isEmpty(),
        picker = picker,
        carousel = carousel,
        fit = s.fit,
    )
}

class MainViewModel(private val index: PhotoIndex, private val clock: Clock) : ViewModel() {
    private val today: LocalDate get() = LocalDate.now(clock)
    private val selection = MutableStateFlow(defaultSelection(today))

    val uiState: StateFlow<UiState> = combine(index.state, selection) { i, s -> buildUiState(i, s, today) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildUiState(index.state.value, selection.value, today))

    init {
        viewModelScope.launch {
            index.state.first { it is IndexState.Ready }
            fit(animate = false)
        }
    }

    fun refresh() = index.refresh()

    fun pick(frame: Frame) {
        if (frame == Frame.Custom) {
            selection.update {
                val minYear = (index.state.value as? IndexState.Ready)?.minYear ?: today.year
                val year = it.custom.to.year.coerceIn(minOf(minYear, today.year), today.year)
                it.copy(picker = Picker(it.custom.from, it.custom.to, year))
            }
        } else {
            selection.update { it.copy(frame = frame) }
            fit()
        }
    }

    fun tapMonth(ym: YearMonth) = selection.update { s -> s.copy(picker = s.picker?.tap(ym)) }

    fun shiftPickerYear(delta: Int) = selection.update { s -> s.copy(picker = s.picker?.let { it.copy(year = it.year + delta) }) }

    fun closePicker() = selection.update { it.copy(picker = null) }

    fun applyPicker() {
        selection.update { s ->
            val p = s.picker ?: return@update s
            s.copy(custom = p.range, frame = Frame.Custom, picker = null)
        }
        fit()
    }

    fun openPlace(id: String) = selection.update { it.copy(placeId = id) }

    fun closePlace() = selection.update { it.copy(placeId = null) }

    fun fit(animate: Boolean = true) = selection.update { it.copy(fit = FitRequest(it.fit.id + 1, animate)) }
}
