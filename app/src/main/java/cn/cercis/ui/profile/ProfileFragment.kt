package cn.cercis.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cn.cercis.MainActivity
import cn.cercis.R
import cn.cercis.databinding.FragmentProfileBinding
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.snackbarMakeSuccess
import cn.cercis.util.validation.validatePassword
import cn.cercis.viewmodel.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.viewModel = profileViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.buttonProfileEdit.setOnClickListener {
            profileViewModel.currentUser.value?.run {
                doDetailNavigation(ProfileEditFragmentDirections.actionGlobalProfileEditFragment(
                    user = this
                ))
            }
        }
        binding.buttonLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                (requireActivity() as MainActivity).logout()
            }
        }
        binding.buttonChangePassword.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("修改密码")
                .setView(R.layout.dialog_change_password)
                .setPositiveButton(R.string.dialog_ok) { dialog, _ -> dialog.dismiss() }
                .setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                .show().let { dialog ->
                    val originLayout =
                        dialog.findViewById<TextInputLayout>(R.id.dialog_change_password_original_password_layout)!!
                    val origin =
                        dialog.findViewById<TextInputEditText>(R.id.dialog_change_password_original_password)!!
                    val newLayout =
                        dialog.findViewById<TextInputLayout>(R.id.dialog_change_password_new_password_layout)!!
                    val new =
                        dialog.findViewById<TextInputEditText>(R.id.dialog_change_password_new_password)!!
                    val confirmLayout =
                        dialog.findViewById<TextInputLayout>(R.id.dialog_change_password_new_password_confirm_layout)!!
                    val confirm =
                        dialog.findViewById<TextInputEditText>(R.id.dialog_change_password_new_password_confirm)!!

                    val passwordError = { password: String ->
                        validatePassword(password).let {
                            when {
                                it.emptyOrValid -> null
                                !it.ruleLength -> cn.cercis.util.getString(R.string.signup_error_password_min_8_max_20)
                                !it.ruleAllowedCharacters -> cn.cercis.util.getString(R.string.signup_error_password_invalid_character)
                                    .format(it.invalidCharacter!!)
                                else -> cn.cercis.util.getString(R.string.signup_error_password_should_3_out_of_4)
                            }
                        }
                    }

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .apply { isEnabled = false; isClickable = false }
                        .setOnClickListener {
                            if (passwordError(new.text.toString()) == null && confirm.text.toString() == new.text.toString()) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    profileViewModel.changePassword(origin.text.toString(),
                                        new.text.toString()).let { res ->
                                        if (res is NetworkResponse.Success) {
                                            launch(Dispatchers.Main) {
                                                try {
                                                    dialog.cancel()
                                                } catch (ignored: Exception) {
                                                }
                                                snackbarMakeSuccess(
                                                    binding.root,
                                                    getString(R.string.profile_snackbar_change_password_success),
                                                    Snackbar.LENGTH_SHORT
                                                )
                                            }
                                        } else {
                                            launch(Dispatchers.Main) {
                                                confirmLayout.error =
                                                    getString(R.string.profile_snackbar_change_password_failed)
                                                        .format(res.message)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    new.doOnTextChanged { text, _, _, _ ->
                        newLayout.error = passwordError(text.toString())
                    }
                    arrayOf(confirm, new).forEach {
                        it.doOnTextChanged { _, _, _, _ ->
                            if (confirm.text.toString() != new.text.toString()) {
                                confirmLayout.error =
                                    getString(R.string.error_change_password_inconsistent)
                            } else {
                                confirmLayout.error = null
                            }
                        }
                    }
                    arrayOf(origin, confirm, new).forEach {
                        it.doOnTextChanged { _, _, _, _ ->
                            val canSubmit = validatePassword(new.text.toString()).valid && confirm.text.toString() == new.text.toString()
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                                isEnabled = canSubmit
                                isClickable = canSubmit
                            }
                        }
                    }
                }
        }
        return binding.root
    }
}
