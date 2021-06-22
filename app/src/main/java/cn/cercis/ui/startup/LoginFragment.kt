package cn.cercis.ui.startup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.SelectLocationActivity
import cn.cercis.SelectedLocation
import cn.cercis.ShowLocationActivity
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentLoginBinding
import cn.cercis.util.helper.enableTransition
import cn.cercis.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class LoginFragment : Fragment() {
    companion object {
        const val REQUEST1 = 12
    }

    private val loginViewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.viewModel = loginViewModel
        binding.loginRootLayout.enableTransition()
        binding.lifecycleOwner = viewLifecycleOwner
        loginViewModel.loginError.observe(viewLifecycleOwner) {
            binding.signupPassword.error = it
        }
        binding.linkResetPassword.setOnClickListener {
//            val it = Intent(requireContext(), ShowLocationActivity::class.java)
//            it.putExtra("location", SelectedLocation(116.32632, 40.00383, ""))
//            startActivityForResult(it, REQUEST1)
            Log.d(LOG_TAG, "reset password not implemented")
            Toast.makeText(context, "暂不支持密码重置", Toast.LENGTH_SHORT).show()
        }
        binding.linkUserSignup.setOnClickListener {
            findNavController().navigate(R.id.signUpFragment)
        }
        loginViewModel.currentUserList.observe(viewLifecycleOwner) {
            binding.loginUserIdList.setAdapter(ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                it.map { item -> item.userId.toString() }
            ))
        }
        (binding.root as ViewGroup).enableTransition()
        return binding.root
    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.d(LOG_TAG, "code: $requestCode, result: $resultCode, data: $data")
//    }
}
