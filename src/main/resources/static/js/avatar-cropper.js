(() => {
  document.addEventListener('DOMContentLoaded', () => {
    const cropContainer = document.querySelector('[data-avatar-cropper]');
    const form = document.querySelector('[data-avatar-form]');
    if (!cropContainer || !form) return;

    const fileInput = form.querySelector('[data-avatar-input]');
    let preview = form.querySelector('[data-avatar-preview]');
    const canvas = cropContainer.querySelector('[data-crop-canvas]');
    const confirmButton = cropContainer.querySelector('[data-crop-confirm]');
    const cancelButtons = cropContainer.querySelectorAll('[data-crop-cancel]');
    const error = cropContainer.querySelector('[data-crop-error]');
    if (!fileInput || !preview || !canvas || !confirmButton) return;

    const context = canvas.getContext('2d');
    const image = new Image();
    let scale = 1;
    let offsetX = 0;
    let offsetY = 0;
    let dragStart = null;
    let previewUrl = null;

    const showError = message => {
      if (!error) return;
      error.textContent = message;
      error.hidden = !message;
    };

    const hideCrop = () => {
      cropContainer.hidden = true;
      form.hidden = false;
      dragStart = null;
    };

    const draw = () => {
      const size = canvas.width;
      context.clearRect(0, 0, size, size);
      context.drawImage(image, offsetX, offsetY,
        image.naturalWidth * scale, image.naturalHeight * scale);
    };

    const resetView = () => {
      const size = canvas.parentElement.offsetWidth;
      if (!size || !image.naturalWidth || !image.naturalHeight) {
        showError('Không thể đọc kích thước ảnh. Vui lòng chọn ảnh khác.');
        return;
      }
      canvas.width = canvas.height = size;
      scale = Math.max(size / image.naturalWidth, size / image.naturalHeight);
      offsetX = (size - image.naturalWidth * scale) / 2;
      offsetY = (size - image.naturalHeight * scale) / 2;
      draw();
    };

    fileInput.addEventListener('change', event => {
      const file = event.target.files[0];
      if (!file) return;
      showError('');
      const reader = new FileReader();
      reader.onerror = () => showError('Không thể đọc ảnh. Vui lòng thử lại.');
      reader.onload = loadEvent => {
        image.onerror = () => showError('Định dạng ảnh không thể hiển thị.');
        image.onload = () => {
          form.hidden = true;
          cropContainer.hidden = false;
          window.requestAnimationFrame(resetView);
        };
        image.src = loadEvent.target.result;
      };
      reader.readAsDataURL(file);
      fileInput.value = '';
    });

    const startDrag = (x, y) => {
      dragStart = { x: x - offsetX, y: y - offsetY };
      canvas.style.cursor = 'grabbing';
    };
    const moveDrag = (x, y) => {
      if (!dragStart) return;
      offsetX = x - dragStart.x;
      offsetY = y - dragStart.y;
      draw();
    };
    const stopDrag = () => {
      dragStart = null;
      canvas.style.cursor = 'grab';
    };

    canvas.addEventListener('mousedown', event => startDrag(event.clientX, event.clientY));
    canvas.addEventListener('mousemove', event => moveDrag(event.clientX, event.clientY));
    canvas.addEventListener('mouseup', stopDrag);
    canvas.addEventListener('mouseleave', stopDrag);
    canvas.addEventListener('touchstart', event => {
      event.preventDefault();
      const touch = event.touches[0];
      startDrag(touch.clientX, touch.clientY);
    }, { passive: false });
    canvas.addEventListener('touchmove', event => {
      event.preventDefault();
      const touch = event.touches[0];
      moveDrag(touch.clientX, touch.clientY);
    }, { passive: false });
    canvas.addEventListener('touchend', stopDrag);
    canvas.addEventListener('wheel', event => {
      event.preventDefault();
      scale = Math.min(4, Math.max(0.5, scale - event.deltaY * 0.001));
      draw();
    }, { passive: false });

    cancelButtons.forEach(button => button.addEventListener('click', hideCrop));
    confirmButton.addEventListener('click', () => {
      const output = document.createElement('canvas');
      output.width = output.height = 256;
      const outputContext = output.getContext('2d');
      outputContext.drawImage(canvas, 0, 0, canvas.width, canvas.width, 0, 0, 256, 256);
      output.toBlob(blob => {
        if (!blob) {
          showError('Không thể xử lý ảnh. Vui lòng thử lại.');
          return;
        }
        const transfer = new DataTransfer();
        transfer.items.add(new File([blob], 'avatar.jpg', { type: 'image/jpeg' }));
        fileInput.files = transfer.files;
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = URL.createObjectURL(blob);
        if (preview.tagName === 'IMG') {
          preview.src = previewUrl;
        } else {
          const imagePreview = document.createElement('img');
          imagePreview.src = previewUrl;
          imagePreview.alt = 'Ảnh đại diện';
          imagePreview.className = 'admin-profile-avatar-image';
          imagePreview.dataset.avatarPreview = '';
          preview.replaceWith(imagePreview);
          preview = imagePreview;
        }
        hideCrop();
      }, 'image/jpeg', 0.92);
    });

    window.addEventListener('beforeunload', () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    });
  });
})();
