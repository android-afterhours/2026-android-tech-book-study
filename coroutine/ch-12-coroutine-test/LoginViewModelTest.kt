import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    sealed interface LoginUiState {
        data object Idle : LoginUiState
        data object Loading : LoginUiState
        data object Success : LoginUiState
    }

    sealed interface LoginEffect {
        data class ShowToast(val message: String) : LoginEffect
    }

    class FakeLoginRepository {
        suspend fun login(id: String, password: String): Boolean {
            delay(100)
            return id == "admin" && password == "1234"
        }
    }

    class LoginViewModel(
        private val repository: FakeLoginRepository
    ) {
        private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
        val uiState: StateFlow<LoginUiState> = _uiState

        private val _effect = MutableSharedFlow<LoginEffect>()
        val effect: SharedFlow<LoginEffect> = _effect

        suspend fun onLoginClick(id: String, password: String) {
            _uiState.value = LoginUiState.Loading

            val isSuccess = repository.login(id, password)
            if (isSuccess) {
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Idle
                _effect.emit(LoginEffect.ShowToast("아이디 또는 비밀번호를 확인해주세요"))
            }
        }
    }

    @Test
    fun `로그인 성공 시 uiState가 Idle에서 Loading을 거쳐 Success로 변경된다`() = runTest {
        val viewModel = LoginViewModel(FakeLoginRepository())
        val states = mutableListOf<LoginUiState>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            // StateFlow는 계속 살아있는 상태 스트림이라 backgroundScope에서 구독 유지
            viewModel.uiState.collect(states::add)
        }

        viewModel.onLoginClick(id = "admin", password = "1234")

        advanceUntilIdle()

        assertEquals(
            listOf(
                LoginUiState.Idle,
                LoginUiState.Loading,
                LoginUiState.Success
            ),
            states
        )
    }

    @Test
    fun `로그인 실패 시 Loading 후 Idle로 돌아가고 토스트 effect를 발행한다`() = runTest {
        val viewModel = LoginViewModel(FakeLoginRepository())
        val states = mutableListOf<LoginUiState>()
        val effects = mutableListOf<LoginEffect>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            // 상태 변화 흐름 관찰
            viewModel.uiState.collect(states::add)
        }

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            // effect는 replay가 없어서 구독을 먼저 확실히 붙여둠
            viewModel.effect.collect(effects::add)
        }

        viewModel.onLoginClick(id = "wrong", password = "wrong")

        advanceUntilIdle()

        assertEquals(
            listOf(
                LoginUiState.Idle,
                LoginUiState.Loading,
                LoginUiState.Idle
            ),
            states
        )

        val expected: List<LoginEffect> = listOf(
            LoginEffect.ShowToast("아이디 또는 비밀번호를 확인해주세요")
        )

        assertEquals(
            expected,
            effects
        )
    }
}
